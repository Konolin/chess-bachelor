import { Component, inject, OnInit } from '@angular/core';
import { GameService } from '../../services/game.service';
import { take } from 'rxjs';
import { Button } from 'primeng/button';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [Button],
  templateUrl: './board.component.html',
  styleUrl: './board.component.css',
})
export class BoardComponent implements OnInit {
  private readonly gameService = inject(GameService);
  gameBoardFEN: string | undefined;

  ngOnInit(): void {
    this.initGameBoard();
  }

  private initGameBoard(): void {
    this.gameService
      .startingGameBoardFEN()
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          this.gameBoardFEN = response.fen;
        },
      });
  }
}
