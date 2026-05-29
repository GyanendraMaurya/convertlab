import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { BroadcastService } from '../../../services/broadcast.service';

@Component({
  selector: 'app-broadcast-snackbar',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './broadcast-snackbar.component.html',
  styleUrl: './broadcast-snackbar.component.scss',
})
export class BroadcastSnackbarComponent {
  readonly broadcastService = inject(BroadcastService);
}
