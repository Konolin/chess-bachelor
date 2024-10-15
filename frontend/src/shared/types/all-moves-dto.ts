import { Move } from './move';

export type AllMovesDTO = {
  allMoves: Move[] | null;
  attackMoves: Move[] | null;
};
