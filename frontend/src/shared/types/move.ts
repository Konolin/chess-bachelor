import { MoveType } from './move-type';

export type Move = {
  fromTileIndex: number;
  toTileIndex: number;
  moveType: MoveType;
};
