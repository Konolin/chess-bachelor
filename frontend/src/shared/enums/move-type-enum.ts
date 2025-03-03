export enum MoveTypeEnum {
  NORMAL = 'NORMAL',
  ATTACK = 'ATTACK',
  EN_PASSANT = 'EN_PASSANT',
  DOUBLE_PAWN_ADVANCE = 'DOUBLE_PAWN_ADVANCE',
  PROMOTION = 'PROMOTION',
  PROMOTION_ATTACK = 'PROMOTION_ATTACK',
  KING_SIDE_CASTLE = 'KING_SIDE_CASTLE',
  QUEEN_SIDE_CASTLE = 'QUEEN_SIDE_CASTLE',
}

export function isAttack(moveType: MoveTypeEnum): boolean {
  return (
    moveType === MoveTypeEnum.ATTACK ||
    moveType === MoveTypeEnum.EN_PASSANT ||
    moveType === MoveTypeEnum.PROMOTION_ATTACK
  );
}

export function isPromotion(moveType: MoveTypeEnum): boolean {
  return moveType === MoveTypeEnum.PROMOTION || moveType === MoveTypeEnum.PROMOTION_ATTACK;
}

export function isCastle(moveType: MoveTypeEnum): boolean {
  return moveType === MoveTypeEnum.KING_SIDE_CASTLE || moveType === MoveTypeEnum.QUEEN_SIDE_CASTLE;
}
