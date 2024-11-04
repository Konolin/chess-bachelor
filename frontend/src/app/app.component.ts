import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MenubarModule } from 'primeng/menubar';
import { MenuItem } from 'primeng/api';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, MenubarModule, TranslocoPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent implements OnInit {
  protected items: MenuItem[] | undefined;

  ngOnInit(): void {
    this.items = [
      {
        label: 'home',
        icon: 'pi pi-home',
        route: '/',
      },
      {
        label: 'play',
        icon: 'pi pi-play',
        route: '/play',
      },
    ];
  }
}
