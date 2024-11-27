import { BitBoards } from './bitboards';

export type BoardState = {
  fen: string;
  winnerFlag: 1 | 0 | -1;
  bitBoards: BitBoards;
};
