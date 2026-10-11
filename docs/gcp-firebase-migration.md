# GCP/Firebase migration runbook

## Target architecture

- Firebase Hosting: public entry point, CDN, TLS, and custom domain
- Cloud Run `time2gather-web`: Astro SSR, request-based billing, minimum instances 0
- Cloud Run `time2gather-api`: Spring Boot API, request-based billing, minimum instances 0
- Firestore `(default)`: durable application data; only the API service account accesses it
- Secret Manager: JWT and optional OpenAI secrets
- Artifact Registry and Cloud Build: container builds

Firebase Hosting serves generated static assets first, forwards `/api/**` to the API,
and forwards every other route to Astro. The anonymous HttpOnly cookie is therefore
first-party and production does not depend on cross-origin cookies.

## Cost guardrails

Blaze is required because Firebase Hosting rewrites to Cloud Run require a billing
account. This is not a fixed zero-cost guarantee. Cloud Run must use request-based
billing and minimum instances 0 so light traffic can remain inside the free allowance.
Only the Firestore `(default)` database receives the free quota.

Do not provision Cloud SQL: it has a recurring instance cost and defeats the near-zero
cost objective.

The production project is `time2gather-prod-2026` (`Time2Gather Production`). On
2026-10-09 the following controls were created and verified:

- `Time2Gather monthly cap`: an enforced Cloud Run spend cap of KRW 5,000 with
  50%, 80%, and 100% notifications. Enforcement is not instantaneous, so a small
  overage is still possible. Cloud Run resumes automatically when the cap resets.
- `Time2Gather total monthly alert`: a project-wide KRW 10,000 alert-only budget
  with 50%, 90%, and 100% notifications to billing users and project owners.

Firestore and Firebase Hosting are not eligible for the enforced service cap in the
current Billing UI. Their usage is covered by the project-wide alert, so the application
must also control traffic, caching, and abusive write rates.

### Monthly estimate

These are planning ranges rather than guarantees. Exchange rates, response sizes, and
traffic patterns affect the final bill.

| Usage profile | Assumptions | Expected monthly cost |
| --- | --- | --- |
| Low traffic | Up to roughly 100k dynamic requests, Hosting transfer at or below 10 GB, Firestore within its daily free quota | KRW 0-1,000 |
| Normal small service | Up to roughly 1M dynamic requests and 20 GB total Hosting transfer, no sustained abuse | KRW 2,000-5,000 |
| Budget-risk boundary | Around 50 GB total Hosting transfer or unusually write-heavy Firestore use | Can approach or exceed KRW 10,000 |

Firebase Hosting includes 10 GB/month of data transfer; additional transfer is
USD 0.15/GB. Firestore's one free database includes 1 GiB stored data, 50,000 reads,
20,000 writes, and 20,000 deletes per day. Cloud Run includes 2 million requests,
180,000 vCPU-seconds, and 360,000 GiB-seconds per month under request-based billing.
Static assets use immutable one-year caching in `firebase.json` to reduce repeat transfer.

Official references:

- https://firebase.google.com/pricing
- https://firebase.google.com/docs/projects/billing/firebase-pricing-plans
- https://firebase.google.com/docs/hosting/cloud-run
- https://cloud.google.com/run/pricing
- https://cloud.google.com/firestore/pricing

## Cutover gates

1. Complete the persistence adapter migration from JPA/MySQL to Firestore.
2. Run backend tests and the frontend production build.
3. Create GCP project `time2gather-prod-2026` and link a healthy billing account. (Done)
4. Enable Cloud Run, Firestore, Firebase Hosting, Artifact Registry, Cloud Build, and Secret Manager.
5. Deploy both services in `asia-northeast3`, with minimum instances 0 and maximum instances 3.
6. Deploy `firebase.json`, Firestore rules, and indexes from the frontend repository.
7. Smoke-test meeting creation, incognito nickname-only join, vote create/update,
   location voting, confirmation, calendar export, and My Meetings.
8. Add the production domain in Firebase Hosting and then replace DNS records in Gabia.
9. Observe errors and core flows before AWS teardown.

## Deployment status (2026-10-10)

- Firestore `(default)` is active in Native mode in `asia-northeast3`; client access
  is denied and the Cloud Run runtime service account has the required server-side
  access.
- Cloud Run API revision `time2gather-api-00003-ln9` and the current
  `time2gather-web` revision are deployed with minimum instances 0 and maximum
  instances 3.
- Firebase Hosting is deployed at `https://time2gather-prod-2026.web.app` and the
  anonymous sign-in, session lookup, meeting creation, and meeting read smoke tests
  passed.
- Firebase custom domains `time2gather.org` and `www.time2gather.org` are registered.
- Gabia DNS contains the Firebase apex A/TXT, `www` CNAME, and both ACME TXT records.
  The domain registrar accepted the authoritative nameserver change from Route 53 to
  `ns.gabia.co.kr`, `ns1.gabia.co.kr`, and `ns.gabia.net`. Registry/recursive DNS
  propagation and Firebase TLS issuance completed on 2026-10-10.
- Both `https://time2gather.org` and `https://www.time2gather.org` serve the Firebase
  deployment over valid HTTPS. Anonymous sign-in, cookie-backed session lookup,
  meeting creation, and meeting retrieval passed through the custom domain.
- AWS teardown and the account-wide follow-up cleanup are complete.

## Runtime configuration

- API: `JWT_SECRET`, Firestore project identity, and optional `OPENAI_API_KEY`
- Web build: `SITE_URL`; omit `PUBLIC_API_URL` in production to use same-origin `/api`
- Firestore: Native mode, `asia-northeast3`, database ID `(default)`

## AWS teardown checklist

Teardown completed on 2026-10-11 after production verification and the user's final
destructive-action confirmation.

- EC2 `i-0848eb9df8d27381a` (`time2gather-app`)
- Elastic IP currently serving `54.116.190.150`
- Attached EBS volumes and snapshots, after checking they are Time2Gather-only
- EBS `vol-0563e9e0e83f96307` (gp3, 20 GiB, instance root volume)
- No load balancer, target group, owned EBS snapshot, or RDS database currently exists in Seoul
- Time2Gather security groups `sg-0cb6885a47b877d82`, `sg-0bfa90eaf86276b3b`,
  plus the orphaned EC2/RDS pair `sg-0317b3ad812cfc73b`, `sg-0d357331905b22a21`
- VPC `vpc-0a256bcb2aa6f7131`, subnets, route tables, and internet/NAT gateways
- ACM certificate `d79b370c-d88f-47a7-9aaf-85041ff956c4` for `time2gather.org`
  (`storify.kr` certificate is unrelated and must be preserved)
- Route 53 zone `Z06584822MPR6G4CQOX2` currently is authoritative and contains apex,
  `www`, and `api` A records pointing to `54.116.190.150`. Move authoritative DNS
  to Gabia and verify propagation before deleting this hosted zone.
- IAM access key and the over-privileged `chatgpt` user/group after migration work

Deleted and verified absent:

- EC2 instance, root EBS volume, and Elastic IP
- Route 53 hosted zone for `time2gather.org`
- Time2Gather ACM certificate
- VPC endpoint, subnets, route tables, internet gateway, four non-default security
  groups, and VPC `vpc-0a256bcb2aa6f7131`
- `chatgpt` IAM access key and user, plus the `time2gather` IAM group

The final Seoul inventory returned no active EC2 instances, EBS volumes, Elastic IPs,
NAT gateways, RDS instances/clusters, or load balancers. Historical current-month
charges remain visible until billing closes, but no Time2Gather AWS resource remains
to accrue future usage.

After a separate account-wide cost cleanup request, the stale `storify.kr` Route 53
hosted zone and the empty `blue-pill` S3 bucket were deleted. On 2026-10-11 the user
explicitly chose not to migrate `blue-pill.me` DNS, so its Route 53 hosted zone was
also deleted; the domain will stop resolving as cached DNS data expires.

The final verification returned zero Route 53 hosted zones and zero S3 buckets. The
Seoul region also returned zero active EC2 instances, EBS volumes, Elastic IPs, NAT
gateways, RDS instances, application/classic load balancers, Lambda functions, ECR
repositories, and CloudWatch Logs groups. Previously accrued and delayed-metering
charges can still appear on the current or final bill even though no identified
resource remains to accrue ongoing AWS usage.
