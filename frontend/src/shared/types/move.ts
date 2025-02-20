import { MoveTypeEnum } from '../enums/move-type-enum';
import { PieceTypeEnum } from '../enums/piece-type-enum';

export type Move = {
  fromTileIndex: number;
  toTileIndex: number;
  moveType: MoveTypeEnum;
  promotedPieceType: PieceTypeEnum | null;
};
