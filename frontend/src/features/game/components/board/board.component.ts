import { Component, inject, OnInit } from '@angular/core';
import { GameService } from '../../services/game.service';
import { take } from 'rxjs';
import { Button } from 'primeng/button';
import { Tile } from '../../../../shared/types/tile';
import { NgClass } from '@angular/common';
import { AllMovesDTO } from '../../../../shared/types/all-moves-dto';
import { BoardDto } from '../../../../shared/types/board-dto';

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
  protected allLegalMoves: AllMovesDTO | null = null;

  private readonly gameService = inject(GameService);
  private previousSelectedTile: Tile | null = null;

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

    // if no previous tile is selected, select the current tile and find all legal moves
    if (!this.previousSelectedTile) {
      this.selectTile(tile);
      return;
    }

    // if a piece is selected but a non-legal tile is selected, move is cancelled
    if (this.allLegalMoves && this.allLegalMoves.allMoves) {
      for (const move of this.allLegalMoves.allMoves) {
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
    let styleClass =
      (Math.floor(tile.index / 8) + tile.index) % 2 === 0 ? 'tile light-tile' : 'tile dark-tile';

    if (this.allLegalMoves && this.allLegalMoves.attackMoves) {
      for (const move of this.allLegalMoves.attackMoves) {
        if (move.toTileIndex === tile.index) {
          return (styleClass += ' attack-move');
        }
      }
    }

    if (this.allLegalMoves && this.allLegalMoves.allMoves) {
      for (const move of this.allLegalMoves.allMoves) {
        if (move.toTileIndex === tile.index) {
          return (styleClass += ' normal-move');
        }
      }
    }

    return styleClass;
  }

  getPieceImageUrl(piece: string): string {
    const color = piece === piece.toUpperCase() ? 'w' : 'b';
    return `assets/pieces/${color}${piece.toLowerCase()}.svg`;
  }

  private makeMove(tile: Tile) {
    this.gameService
      .makeMove(this.previousSelectedTile!.index, tile.index)
      .pipe(take(1))
      .subscribe((response) => {
        this.updateGameState(response);
      });
    this.resetMove();
  }

  private selectTile(tile: Tile): void {
    this.previousSelectedTile = tile;
    this.gameService
      .fetchLegalMoves(tile.index)
      .pipe(take(1))
      .subscribe((response) => (this.allLegalMoves = response));
  }

  private isInvalidSelection(tile: Tile): boolean {
    return !tile.occupiedByString && !this.previousSelectedTile;
  }

  private selectAnotherPiece(tile: Tile) {
    this.previousSelectedTile = null;
    this.onTileClicked(tile);
  }

  private resetMove() {
    this.previousSelectedTile = null;
    this.allLegalMoves = null;
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

  private updateGameState(boardState: BoardDto) {
    this.tiles = this.gameService.FENtoTileArray(boardState.fen);
  }
}
