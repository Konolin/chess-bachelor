import { Routes } from '@angular/router';
import { BoardComponent } from '../../features/game/components/board/board.component';
import { HomeComponent } from '../home/home.component';
import { NotFoundComponent } from '../not-found/not-found.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'play', component: BoardComponent },
  { path: '**', component: NotFoundComponent },
];
