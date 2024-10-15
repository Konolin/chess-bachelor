import { Component, inject, OnInit } from '@angular/core';
import { GameService } from '../../services/game.service';
import { take } from 'rxjs';
import { Button } from 'primeng/button';
import { Tile } from '../../../../shared/types/tile';
import { NgClass } from '@angular/common';
import { AllMovesDTO } from '../../../../shared/types/all-moves-dto';

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
    // if an empty tile is clicked, do nothing
    if (!tile.occupiedByString) {
      return;
    }

    // if the current tile is the same as the previous tile, deselect it
    if (this.previousSelectedTile === tile) {
      this.previousSelectedTile = null;
      this.allLegalMoves = null;
      return;
    }

    // if no previous tile is selected, select the current tile and find all legal moves
    if (!this.previousSelectedTile) {
      this.previousSelectedTile = tile;
      // get all legal moves of the selected piece
      this.gameService
        .fetchLegalMoves(tile.index)
        .pipe(take(1))
        .subscribe((response) => (this.allLegalMoves = response));
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

  private initGameBoard(): void {
    // fetch board FEN
    this.gameService
      .fetchStartingGameBoardFEN()
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          // transform FEN in an array of strings
          this.tiles = this.gameService.FENtoTileArray(response.fen);
        },
      });
  }
}
