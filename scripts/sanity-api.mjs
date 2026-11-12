import assert from 'node:assert/strict'
import fs from 'node:fs'
const base=process.env.SANITY_API_BASE || 'http://127.0.0.1:18081/api'
assert.ok(['localhost','127.0.0.1'].includes(new URL(base).hostname),'Sanity writes are local-only')
let checks=0
async function request(path,{method='GET',body,cookie,lang='en',status=200}={}){
 const response=await fetch(base+path,{method,headers:{'Content-Type':'application/json','Accept-Language':lang,...(cookie?{Cookie:cookie}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})})
 const text=await response.text();assert.equal(response.status,status,`${method} ${path}: ${text}`);checks++
 return {data:response.headers.get('content-type')?.includes('json')?JSON.parse(text):text,cookie:response.headers.get('set-cookie')?.split(';')[0]}
}
const login=async name=>(await request('/v1/auth/anonymous',{method:'POST',body:{username:name}})).cookie
const host=await login('Local sanity host'), member=await login('Local sanity participant')
await request('/v1/auth/me',{cookie:host})
await request('/v1/meetings',{method:'POST',body:{},status:401})
const create=async(body,cookie=host)=>(await request('/v1/meetings',{method:'POST',cookie,body})).data.data.meetingCode
const code=await create({title:'Sanity & 東京 모임 #1',timezone:'Asia/Kathmandu',selectionType:'TIME',availableDates:{'2027-01-01':['00:00','01:00','02:00']},locationVoteEnabled:true,locations:['Tokyo 東京','Kathmandu काठमाडौं']})
let detail=(await request('/v1/meetings/'+code)).data.data
const location=detail.locationVote.locations[0].id
await request(`/v1/meetings/${code}/selections`,{method:'PUT',cookie:host,body:{selections:[{date:'2027-01-01',type:'TIME',times:['00:00','01:00']}]}})
await request(`/v1/meetings/${code}/selections`,{method:'PUT',cookie:member,body:{selections:[{date:'2027-01-01',type:'TIME',times:['00:00']}]}})
await request(`/v1/meetings/${code}/location-selections`,{method:'PUT',cookie:member,body:{locationIds:[location]}})
assert.equal((await request(`/v1/meetings/${code}/location-selections`,{cookie:member})).data.data.locationIds[0],location)
await request(`/v1/meetings/${code}/confirm`,{method:'PUT',cookie:member,body:{date:'2027-01-01',slotIndex:0,locationId:location},status:403})
await request(`/v1/meetings/${code}/confirm`,{method:'PUT',cookie:host,body:{date:'2027-01-01',slotIndex:0,locationId:location}})
detail=(await request('/v1/meetings/'+code)).data.data
assert.equal(detail.meeting.confirmedTime,'00:00');assert.equal(detail.summary.bestSlots[0].count,2)
assert.match((await request(`/v1/meetings/${code}/export`)).data,/DTSTART:20261231T181500Z/)
await request(`/v1/meetings/${code}/selections`,{method:'PUT',cookie:member,body:{selections:[]},status:400})
await request(`/v1/meetings/${code}/confirmed-time`,{method:'PUT',cookie:host,body:{time:'00:15'}})
assert.match((await request(`/v1/meetings/${code}/export`)).data,/DTSTART:20261231T183000Z/)
detail=(await request('/v1/meetings/'+code)).data.data
assert.equal(detail.meeting.confirmedSlotIndex,0)
fs.writeFileSync('/tmp/t2g-sanity-api-result.json',JSON.stringify({meetingCode:code,detail},null,2))
const day=await create({title:'Date only sanity',timezone:'America/Los_Angeles',selectionType:'ALL_DAY',availableDates:{'2027-01-03':null,'2027-01-04':null}})
await request(`/v1/meetings/${day}/selections`,{method:'PUT',cookie:member,body:{selections:[{date:'2027-01-03',type:'ALL_DAY'}]}})
await request(`/v1/meetings/${day}/confirm`,{method:'PUT',cookie:host,body:{date:'2027-01-03'}})
const ics=(await request(`/v1/meetings/${day}/export`)).data;assert.match(ics,/DTSTART;VALUE=DATE:20270103/);assert.match(ics,/DTEND;VALUE=DATE:20270104/)
await request(`/v1/meetings/${day}/confirm`,{method:'DELETE',cookie:host})
await request(`/v1/meetings/${day}/available-dates`,{method:'PUT',cookie:host,body:{dates:['2027-01-04','2027-01-05']}})
assert.ok(!(await request(`/v1/meetings/${day}/selections`,{cookie:member})).data.data.selections['2027-01-03'])
for(const lang of ['en','zh-CN','hi','es','ar','fr','bn','pt','ru','id','ur','de','ja','pcm','ar-EG','mr','vi','te','ha','tr','ko','th']){
 const error=await request('/v1/meetings/local-missing',{lang,status:404});assert.ok(error.data.message);assert.ok(!error.data.message.startsWith('error.'))
}
for(const [date,time,key] of [['2027-03-14','02:00','error.meeting.time.nonexistent'],['2027-11-07','01:00','error.meeting.time.ambiguous']]){
 const response=await request('/v1/meetings',{method:'POST',cookie:host,body:{title:'DST check',timezone:'America/New_York',selectionType:'TIME',availableDates:{[date]:[time]}},status:400});assert.equal(response.data.messageKey,key)
}
const night=await create({title:'Midnight boundary sanity',timezone:'UTC',selectionType:'TIME',availableDates:{'2027-01-02':['22:00','23:00']}})
await request(`/v1/meetings/${night}/selections`,{method:'PUT',cookie:host,body:{selections:[{date:'2027-01-02',type:'TIME',times:['22:00','23:00']}]}})
const nightDetail=(await request(`/v1/meetings/${night}`)).data.data
assert.equal(nightDetail.summary.bestSlots[0].time,'22:00 ~ 00:00')
assert.match((await request(`/v1/meetings/${night}/export?date=2027-01-02&slotIndex=23`)).data,/DTEND:20270103T000000Z/)
const mine=(await request('/v1/auth/me',{cookie:host})).data.data;assert.ok(mine.createdMeetings.some(meeting=>meeting.code===code))
await request('/v1/admin/summary',{cookie:member,status:401})
console.log(`Local API sanity passed: ${checks} HTTP checks; TIME/ALL_DAY, sessions, votes, location, confirmation/edit/cancel, ICS, dates, 22 locale errors, DST and authorization. UI fixture code: ${code}`)
