// ...existing code...
async getCurrentUser(userId: string) {
  const user = await this.userService.findById(userId);
  if (!user) {
    throw new NotFoundException('사용자를 찾을 수 없습니다.');
  }

  let meetingCode: string | undefined;

  // 익명 유저인 경우 미팅 코드 조회
  if (user.isAnonymous) {
    const participant = await this.prisma.participant.findFirst({
      where: { userId: user.id },
      include: { meeting: true },
      orderBy: { createdAt: 'desc' },
    });
    meetingCode = participant?.meeting?.meetingCode;
  }

  return {
    id: user.id,
    email: user.email,
    name: user.name,
    isAnonymous: user.isAnonymous,
    meetingCode,
  };
}
// ...existing code...

