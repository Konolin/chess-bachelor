import { inject, Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root',
})
export class CustomToastrService {
  private readonly toastrService = inject(ToastrService);

  info(message: string, title: string) {
    this.toastrService.info(message, title, { timeOut: 3000 });
  }

  error(message: string, title: string) {
    this.toastrService.error(message, title, { disableTimeOut: true });
  }
}
