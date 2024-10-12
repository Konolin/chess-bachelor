import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoardState } from '../../../shared/types/board-state';
import { Tile } from '../../../shared/types/tile';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);

  fetchStartingGameBoardFEN(): Observable<BoardState> {
    return this.http.get<BoardState>('http://localhost:8080/api/game/starting-board-state');
  }

  fetchLegalMovesIndexes(tileIndex: number) {
    const params = new HttpParams().set('tileIndex', tileIndex); // Assuming 'tile.id' is the property you want to send
    return this.http.get<number[]>('http://localhost:8080/api/game/get-legal-moves-indexes', {
      params,
    });
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
        tileArray.push({ index: index, occupiedByString: char });
        index++;
        continue;
      }

      // add empty tiles
      for (let i = 0; i < parseInt(char); i++) {
        tileArray.push({ index: index, occupiedByString: '' });
        index++;
      }
    }

    return tileArray;
  }
}
