import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoardState } from '../../../shared/types/board-state';
import { Tile } from '../../../shared/types/tile';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);

  startingGameBoardFEN(): Observable<BoardState> {
    return this.http.get<BoardState>('http://localhost:8080/api/game/starting-board-state');
  }

  FENtoTileArray(fen: string): Tile[] {
    const tileArray: Tile[] = [];
    let index = 0;

    for (const char of fen) {
      // skip breaking characters
      if (char === '/') {
        continue;
      }
      // add occupied tiles
      if (isNaN(parseInt(char))) {
        tileArray.push({ id: index, occupiedBy: char });
        index++;
        continue;
      }

      // add empty tiles
      for (let i = 0; i < parseInt(char); i++) {
        tileArray.push({ id: index, occupiedBy: '' });
        index++;
      }
    }

    return tileArray;
  }
}
