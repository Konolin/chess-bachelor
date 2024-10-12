import { Component, inject, OnInit } from '@angular/core';
import { GameService } from '../../services/game.service';
import { take } from 'rxjs';
import { Button } from 'primeng/button';
import { Tile } from '../../../../shared/types/tile';
import { NgClass } from '@angular/common';

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
  protected legalMovesIndexes: number[] = [];

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
      this.legalMovesIndexes = [];
      return;
    }

    // if no previous tile is selected, select the current tile and find all legal moves
    if (!this.previousSelectedTile) {
      this.previousSelectedTile = tile;
      // get all legal moves of the selected piece
      this.gameService
        .fetchLegalMovesIndexes(tile.index)
        .pipe(take(1))
        .subscribe((response) => (this.legalMovesIndexes = response));
    }
  }

  getTileClasses(tile: Tile): string {
    if (this.legalMovesIndexes.includes(tile.index)) {
      return 'tile legal-move-tile';
    }
    if ((Math.floor(tile.index / 8) + tile.index) % 2 === 0) {
      return 'tile light-tile';
    }
    return 'tile dark-tile';
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
