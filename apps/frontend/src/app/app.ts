import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { VungThongBao } from './shared/ui/thong-bao';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, VungThongBao],
  template: '<router-outlet /><app-thong-bao />',
})
export class App {}
