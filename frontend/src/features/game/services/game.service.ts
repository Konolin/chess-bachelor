import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BoardState } from '../../../shared/types/board-state';

@Injectable({
  providedIn: 'root',
})
export class GameService {
  private readonly http = inject(HttpClient);

  startingGameBoardFEN(): Observable<BoardState> {
    return this.http.get<BoardState>('http://localhost:8080/api/game/starting-board-state');
  }
}
