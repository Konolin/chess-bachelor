import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoardState } from '../../../shared/types/board-state';
import { Tile } from '../../../shared/types/tile';
import { LegalMovesDto } from '../../../shared/types/legal-moves-dto';
import { FenObject } from '../../../shared/types/fen-object';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);

  fetchStartingGameBoardFEN(): Observable<BoardState> {
    return this.http.get<BoardState>('http://localhost:8080/api/game/starting-board-state');
  }

  fetchLegalMoves(tileIndex: number): Observable<LegalMovesDto> {
    const params = new HttpParams().set('tileIndex', tileIndex);
    return this.http.get<LegalMovesDto>('http://localhost:8080/api/game/get-moves-for-position', {
      params,
    });
  }

  makeMove(fromTileIndex: number, toTileIndex: number): Observable<BoardState> {
    const params = new HttpParams()
      .set('fromTileIndex', fromTileIndex)
      .set('toTileIndex', toTileIndex);
    return this.http.get<BoardState>('http://localhost:8080/api/game/make-move', {
      params,
    });
  }

  FENStringToObject(fen: string): FenObject {
    const tileArray: Tile[] = [];
    let moveMaker = '';
    let index = 0;
    let segmentsRead = 0;

    for (const char of fen) {
      // skip breaking characters
      if (char === '/') {
        continue;
      }
      if (char === ' ') {
        segmentsRead += 1;
        continue;
      }
      // add occupied tiles
      if (segmentsRead === 0 && isNaN(parseInt(char))) {
        tileArray.push({ index: index, occupiedByString: char });
        index++;
        continue;
      }
      // add empty tiles
      for (let i = 0; i < parseInt(char); i++) {
        tileArray.push({ index: index, occupiedByString: '' });
        index++;
      }
      // add moveMaker
      if (segmentsRead === 1) {
        moveMaker = char;
        index++;
      }
    }

    return { tiles: tileArray, moveMaker: moveMaker };
  }
}
