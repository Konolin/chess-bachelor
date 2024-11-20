export enum MoveType {
  NORMAL = 'NORMAL',
  ATTACK = 'ATTACK',
  EN_PASSANT = 'EN_PASSANT',
  DOUBLE_PAWN_ADVANCE = 'DOUBLE_PAWN_ADVANCE',
  PROMOTION = 'PROMOTION',
  PROMOTION_ATTACK = 'PROMOTION_ATTACK',
  KING_SIDE_CASTLE = 'KING_SIDE_CASTLE',
  QUEEN_SIDE_CASTLE = 'QUEEN_SIDE_CASTLE',
}

export function isAttack(moveType: MoveType): boolean {
  return (
    moveType === MoveType.ATTACK ||
    moveType === MoveType.EN_PASSANT ||
    moveType === MoveType.PROMOTION_ATTACK
  );
}

export function isPromotion(moveType: MoveType): boolean {
  return moveType === MoveType.PROMOTION || moveType === MoveType.PROMOTION_ATTACK;
}

export function isCastle(moveType: MoveType): boolean {
  return moveType === MoveType.KING_SIDE_CASTLE || moveType === MoveType.QUEEN_SIDE_CASTLE;
}
