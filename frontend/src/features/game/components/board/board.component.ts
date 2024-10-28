import { Component, inject, OnInit } from '@angular/core';
import { GameService } from '../../services/game.service';
import { take } from 'rxjs';
import { Button } from 'primeng/button';
import { Tile } from '../../../../shared/types/tile';
import { NgClass } from '@angular/common';
import { BoardState } from '../../../../shared/types/board-state';
import { Move } from '../../../../shared/types/move';
import { MoveType } from '../../../../shared/types/move-type';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [Button, NgClass],
  templateUrl: './board.component.html',
  styleUrl: './board.component.css',
})
export class BoardComponent implements OnInit {
  protected tiles: Tile[] = [];
  protected readonly Math = Math;
  protected legalMoves: Move[] | null = null;

  private readonly gameService = inject(GameService);
  private previousSelectedTile: Tile | null = null;
  private moveMaker: string | null = null;

  ngOnInit(): void {
    this.initGameBoard();
  }

  onTileClicked(tile: Tile): void {
    // if an empty tile is clicked and no piece was previously selected, do nothing
    if (this.isInvalidSelection(tile)) {
      return;
    }

    // if the current tile is the same as the previous tile, deselect it
    if (this.previousSelectedTile === tile) {
      this.resetMove();
      return;
    }

    // if no previous tile is selected and the correct alliance is selected,
    // select the current tile and find all legal moves
    if (!this.previousSelectedTile && this.isCorrectAlliance(tile)) {
      this.selectTileAndFetchMoves(tile);
      return;
    }

    if (this.legalMoves) {
      for (const move of this.legalMoves) {
        if (move.toTileIndex === tile.index) {
          // the destination tile is a valid move, execute move
          this.makeMove(tile);
          return;
        }
      }
      // if another friendly piece is selected, show the legal moves for that piece
      if (tile.occupiedByString) {
        this.selectAnotherPiece(tile);
        return;
      }

      // the destination tile is not a legal move
      this.resetMove();
    }
  }

  getTileClasses(tile: Tile): string {
    const styleClass =
      (Math.floor(tile.index / 8) + tile.index) % 2 === 0 ? 'tile light-tile' : 'tile dark-tile';

    const move = this.legalMoves?.find((move) => move.toTileIndex === tile.index);
    if (move) {
      return styleClass + (move.moveType === MoveType.ATTACK ? ' attack-move' : ' normal-move');
    }

    return styleClass;
  }

  getPieceImageUrl(piece: string): string {
    const color = piece === piece.toUpperCase() ? 'w' : 'b';
    return `assets/pieces/${color}${piece.toLowerCase()}.svg`;
  }

  private makeMove(tile: Tile): void {
    this.gameService
      .makeMove(this.previousSelectedTile!.index, tile.index)
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });
    this.resetMove();
  }

  private selectTileAndFetchMoves(tile: Tile): void {
    this.previousSelectedTile = tile;
    this.gameService
      .fetchLegalMoves(tile.index)
      .pipe(take(1))
      .subscribe((response) => (this.legalMoves = response.legalMoves));
  }

  private isInvalidSelection(tile: Tile): boolean {
    return !tile.occupiedByString && !this.previousSelectedTile;
  }

  private selectAnotherPiece(tile: Tile): void {
    this.previousSelectedTile = null;
    this.onTileClicked(tile);
  }

  private isCorrectAlliance(tile: Tile): boolean {
    const isWhitePiece = /^[A-Z]+$/.test(tile.occupiedByString);
    const isBlackPiece = /^[a-z]+$/.test(tile.occupiedByString);

    return (isWhitePiece && this.moveMaker === 'w') || (isBlackPiece && this.moveMaker === 'b');
  }

  private resetMove() {
    this.previousSelectedTile = null;
    this.legalMoves = null;
  }

  private initGameBoard(): void {
    // fetch board FEN
    this.gameService
      .fetchStartingGameBoardFEN()
      .pipe(take(1))
      .subscribe((response) => {
        // transform FEN in an array of strings
        this.updateGameState(response);
      });
  }

  private updateGameState(boardState: BoardState) {
    const fenObject = this.gameService.FENStringToObject(boardState.fen);
    this.tiles = fenObject.tiles;
    this.moveMaker = fenObject.moveMaker;
  }
}
