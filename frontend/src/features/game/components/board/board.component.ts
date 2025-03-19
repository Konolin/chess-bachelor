import { Component, inject, OnInit } from '@angular/core';
import { GameService } from '../../services/game.service';
import { take } from 'rxjs';
import { Button } from 'primeng/button';
import { Tile } from '../../../../shared/types/tile';
import { NgClass } from '@angular/common';
import { BoardState } from '../../../../shared/types/board-state';
import { Move } from '../../../../shared/types/move';
import { isAttack, isCastle, isPromotion } from '../../../../shared/enums/move-type-enum';
import { DialogModule } from 'primeng/dialog';
import { ImageModule } from 'primeng/image';
import { getPieceTypeFromChar } from '../../../../shared/enums/piece-type-enum';

/**
 * BoardComponent: Main UI component for displaying a chess game board and handling game interactions.
 */
@Component({
  selector: 'app-board',
  standalone: true,
  imports: [Button, NgClass, DialogModule, ImageModule],
  templateUrl: './board.component.html',
  styleUrl: './board.component.css',
})
export class BoardComponent implements OnInit {
  protected tiles: Tile[] = [];
  protected legalMoves: Move[] | null = null;
  protected isPromotionMove: boolean = false;
  protected promotionTile: Tile | null = null;
  protected promotionPieces: string[] = ['q', 'r', 'b', 'n'];
  protected winnerFlag: -1 | 0 | 1 = 0;
  protected isWinnerDialogVisible: boolean = false;
  protected rowNames = ['8', '7', '6', '5', '4', '3', '2', '1'];
  protected columnNames = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

  private readonly gameService = inject(GameService);
  private previousSelectedTile: Tile | null = null;
  private moveMaker: string | null = null;
  private previousMove: Move | null = null;

  /** Initialize the game board on component load */
  ngOnInit(): void {
    this.initGameBoard();
  }

  /**
   * Converts the winner flag to a string indicating the winner ("White" or "Black").
   * @returns The winner as a string.
   */
  winnerFlagToString() {
    return this.winnerFlag === 1 ? 'White' : 'Black';
  }

  /**
   * Handles tile clicks, selecting or moving a piece as appropriate.
   * @param currentlySelectedTile The tile that was clicked.
   */
  onTileClicked(currentlySelectedTile: Tile): void {
    if (this.previousSelectedTile) {
      // check if clicked tile is a friendly piece and fetch its moves
      if (!!currentlySelectedTile.occupiedByString && this.isFriendlyPiece(currentlySelectedTile)) {
        this.selectTileAndFetchMoves(currentlySelectedTile);
        return;
      }

      // attempt to find a valid move from the previously selected tile to the currently selected tile
      const move = this.legalMoves!.find(
        (m) =>
          m.fromTileIndex === this.previousSelectedTile!.index &&
          m.toTileIndex === currentlySelectedTile.index
      );

      if (move) {
        this.makeMove(move);
      } else {
        // reset selection if no valid move
        this.resetSelection();
      }

      return;
    }

    // select tile if it contains a friendly piece
    if (!!currentlySelectedTile.occupiedByString && this.isFriendlyPiece(currentlySelectedTile)) {
      this.selectTileAndFetchMoves(currentlySelectedTile);
    }
  }

  /**
   * Calculates and returns CSS classes for a given tile based on its position and move possibilities.
   * @param tile The tile to style.
   * @returns A string with the appropriate CSS classes.
   */
  calculateTileStyleClasses(tile: Tile): string {
    let styleClass: string = 'tile';

    styleClass +=
      tile.index === this.previousMove?.toTileIndex ||
      tile.index === this.previousMove?.fromTileIndex
        ? ' previous-'
        : ' ';
    styleClass += (Math.floor(tile.index / 8) + tile.index) % 2 === 0 ? 'light-tile' : 'dark-tile';

    const move = this.legalMoves?.find((move) => move.toTileIndex === tile.index);
    if (move) {
      return styleClass + (isAttack(move.moveType) ? ' attack-move' : ' normal-move');
    }

    return styleClass;
  }

  /**
   * Returns the image URL for a specific piece based on its type and color.
   * @param piece The piece type (e.g., "q" for queen).
   * @param color The color of the piece ('w' for white, 'b' for black).
   * @returns The URL string for the piece image.
   */
  getPieceImageUrl(piece: string, color: 'w' | 'b'): string {
    return `assets/pieces/${color}${piece.toLowerCase()}.svg`;
  }

  /**
   * Handles piece selection during promotion, completing the saved move.
   * @param piece The selected promotion piece type (e.g., "q" for queen).
   * @param pieceColor The color of the piece ('w' or 'b').
   */
  onPromotedPieceSelection(piece: string, pieceColor: 'w' | 'b'): void {
    this.isPromotionMove = false;
    this.promotionTile = null;

    if (pieceColor === 'w') {
      piece = piece.toUpperCase();
    }

    // Complete the promotion move by adding the selected piece
    this.previousMove!.promotedPieceType = getPieceTypeFromChar(piece);

    this.gameService
      .makeMove(this.previousMove!)
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });

    this.resetSelection();
  }

  /**
   * Makes a move on the board, updating the game state, handling promotions if applicable.
   * Promotion moves DO NOT executed an api call to the engine in this method. The onPromotedPiece
   * function needs to be called right after this one, to receive a piece to promote to and to
   * execute the call that updates the game state.
   * At the end of the function, a request is made to the engine to make a move for the computer player.
   *
   * @param move The move to make.
   */
  private makeMove(move: Move): void {
    if (isPromotion(move.moveType)) {
      // the UI for the promotion will be shown
      this.isPromotionMove = true;
      this.promotionTile = this.tiles[move.toTileIndex];
      this.previousMove = move;
      // returns without completing the move. This allows the onPromotedPieceSelection to receive
      // the desired promotion piece as input and execute the api call itself
      return;
    }

    // perform the move without promotion
    this.gameService
      .makeMove(move)
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });

    this.previousMove = move;

    // edit some attributes to have a different highlight of previous castle moves
    if (isCastle(move.moveType)) {
      this.previousMove.toTileIndex =
        move.toTileIndex + (move.toTileIndex > move.fromTileIndex ? 1 : -2);
    }

    this.resetSelection();

    // computer makes a move after player
    this.gameService
      .computerMakeMove()
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });
  }

  /**
   * Undoes the last move and last move by using the service to contact the server.
   * Updates the boardState.
   */
  protected undoLastMove(): void {
    this.gameService
      .undoLastMove()
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });
  }

  /**
   * Selects a tile and fetches legal moves for the piece on that tile.
   * @param tile The tile to select.
   */
  private selectTileAndFetchMoves(tile: Tile): void {
    this.previousSelectedTile = tile;
    this.gameService
      .fetchLegalMoves(tile.index)
      .pipe(take(1))
      .subscribe((response) => (this.legalMoves = response));
  }

  /**
   * Determines if a tile contains a piece that is friendly to the current player.
   * @param tile The tile to check.
   * @returns True if the piece on the tile is friendly; otherwise, false.
   */
  private isFriendlyPiece(tile: Tile): boolean {
    const isWhitePiece = /^[A-Z]+$/.test(tile.occupiedByString);
    const isBlackPiece = /^[a-z]+$/.test(tile.occupiedByString);

    return (isWhitePiece && this.moveMaker === 'w') || (isBlackPiece && this.moveMaker === 'b');
  }

  /**
   * Resets the selected tile and legal moves, clearing any active selection on the board.
   */
  private resetSelection() {
    this.previousSelectedTile = null;
    this.legalMoves = null;
  }

  /**
   * Initializes the game board by fetching the initial BoardState and updating the game state.
   */
  private initGameBoard(): void {
    this.gameService
      .fetchStartingBoardState()
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });
  }

  /**
   * Updates the game state with a new board configuration and checks for a winner.
   * @param boardState The new board state to apply.
   */
  private updateGameState(boardState: BoardState) {
    this.winnerFlag = boardState.winnerFlag;
    this.isWinnerDialogVisible = this.winnerFlag !== 0;

    const fenObject = this.gameService.FENStringToObject(boardState.fen);
    this.tiles = fenObject.tiles;
    this.moveMaker = fenObject.moveMaker;
  }
}
