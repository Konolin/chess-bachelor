import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoardState } from '../../../shared/types/board-state';
import { Tile } from '../../../shared/types/tile';
import { FenObject } from '../../../shared/types/fen-object';
import { Move } from '../../../shared/types/move';

/**
 * GameService: Manages HTTP requests for game operations and provides helper methods
 * to convert data formats (e.g., FEN) into objects for use within the Angular application.
 */
@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);

  /**
   * Fetches the initial board state from the server.
   * @returns An Observable containing the initial BoardState object.
   */
  fetchStartingBoardState(): Observable<BoardState> {
    return this.http.get<BoardState>('http://localhost:8080/api/game/starting-board-state');
  }

  /**
   * Fetches legal moves for a specific tile, based on the tile index.
   * @param tileIndex The index of the tile for which legal moves are requested.
   * @returns An Observable containing a LegalMovesDto with possible moves for the tile.
   */
  fetchLegalMoves(tileIndex: number): Observable<Move[]> {
    const params = new HttpParams().set('tileIndex', tileIndex);
    return this.http.get<Move[]>('http://localhost:8080/api/game/get-moves-for-position', {
      params,
    });
  }

  /**
   * Sends a move to the server to update the game state.
   * @param move The move to make, including details like the source and destination tile.
   * @returns An Observable containing the updated BoardState after the move is made.
   */
  makeMove(move: Move): Observable<BoardState> {
    return this.http.post<BoardState>('http://localhost:8080/api/game/make-move', move);
  }

  /**
   * Converts a FEN string to a FenObject that represents the board state and the current move maker.
   * The method parses each segment of the FEN to create an array of tiles and determines the current player.
   * @param fen The FEN string representing the board layout and game state.
   * @returns A FenObject with an array of tiles and the current move maker.
   */
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

      if (segmentsRead === 2) {
        break;
      }
    }

    return { tiles: tileArray, moveMaker: moveMaker };
  }
}
