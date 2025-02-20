export enum PieceTypeEnum {
  PAWN = 'PAWN',
  KNIGHT = 'KNIGHT',
  BISHOP = 'BISHOP',
  ROOK = 'ROOK',
  QUEEN = 'QUEEN',
  KING = 'KING',
}

export function getPieceTypeFromChar(pieceChar: string): PieceTypeEnum {
  switch (pieceChar) {
    case 'p':
    case 'P':
      return PieceTypeEnum.PAWN;
    case 'k':
    case 'K':
      return PieceTypeEnum.KING;
    case 'r':
    case 'R':
      return PieceTypeEnum.ROOK;
    case 'q':
    case 'Q':
      return PieceTypeEnum.QUEEN;
    case 'n':
    case 'N':
      return PieceTypeEnum.KNIGHT;
    case 'b':
    case 'B':
      return PieceTypeEnum.BISHOP;
    default:
      throw new Error(`Unknown Piece Char: ${pieceChar}`);
  }
}
