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
  tiles: Tile[] = [];

  private readonly gameService = inject(GameService);

  ngOnInit(): void {
    this.initGameBoard();
  }

  private initGameBoard(): void {
    // fetch board FEN
    this.gameService
      .startingGameBoardFEN()
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          // transform FEN in an array of strings
          this.tiles = this.gameService.FENtoTileArray(response.fen);
        },
      });
  }

  protected readonly Math = Math;
}
