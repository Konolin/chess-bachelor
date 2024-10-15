import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoardDto } from '../../../shared/types/board-dto';
import { Tile } from '../../../shared/types/tile';
import { AllMovesDTO } from '../../../shared/types/all-moves-dto';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);

  fetchStartingGameBoardFEN(): Observable<BoardDto> {
    return this.http.get<BoardDto>('http://localhost:8080/api/game/starting-board-state');
  }

  fetchLegalMoves(tileIndex: number): Observable<AllMovesDTO> {
    const params = new HttpParams().set('tileIndex', tileIndex); // Assuming 'tile.id' is the property you want to send
    return this.http.get<AllMovesDTO>('http://localhost:8080/api/game/get-legal-moves-indexes', {
      params,
    });
  }

  makeMove(fromTileIndex: number, toTileIndex: number): Observable<BoardDto> {
    const params = new HttpParams()
      .set('fromTileIndex', fromTileIndex)
      .set('toTileIndex', toTileIndex);
    return this.http.get<BoardDto>('http://localhost:8080/api/game/make-move', {
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
      // TODO - temporary, add this functionality later
      if (char === ' ') {
        break;
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
