// ...existing code...
export class CurrentUserResponseDto {
  // ...existing code...
  @ApiProperty({
    description: '익명 유저가 로그인한 미팅 코드 (익명 유저만 해당)',
    example: 'ABC123',
    required: false,
  })
  meetingCode?: string;
}

