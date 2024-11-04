import { Component, OnInit } from '@angular/core';
import { MenubarModule } from 'primeng/menubar';
import { MenuItem, PrimeTemplate } from 'primeng/api';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [MenubarModule, PrimeTemplate, TranslocoPipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent implements OnInit {
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
