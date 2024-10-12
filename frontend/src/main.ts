import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './core/app/app.config';
import { AppComponent } from './core/app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
