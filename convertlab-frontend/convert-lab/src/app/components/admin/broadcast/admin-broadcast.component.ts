import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { BroadcastMessage, BroadcastService } from '../../../services/broadcast.service';
import { SnackbarService } from '../../../services/snackbar.service';

interface ExpiryOption {
  label: string;
  value: string;
  minutes?: number;
}

@Component({
  selector: 'app-admin-broadcast',
  imports: [
    CommonModule,
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './admin-broadcast.component.html',
  styleUrl: './admin-broadcast.component.scss',
})
export class AdminBroadcastComponent {
  private readonly fb = inject(FormBuilder);
  private readonly broadcastService = inject(BroadcastService);
  private readonly snackbar = inject(SnackbarService);

  readonly expiryOptions: ExpiryOption[] = [
    { label: '15 minutes', value: '15', minutes: 15 },
    { label: '30 minutes', value: '30', minutes: 30 },
    { label: '1 hour', value: '60', minutes: 60 },
    { label: '24 hours', value: '1440', minutes: 1440 },
    { label: 'Custom date and time', value: 'custom' },
  ];

  readonly broadcastForm = this.fb.nonNullable.group({
    message: ['', [Validators.required, Validators.maxLength(500)]],
    expiryPreset: ['15', [Validators.required]],
    customExpiresAt: [''],
  });

  readonly broadcasts = signal<BroadcastMessage[]>([]);
  readonly loading = signal(true);
  readonly sending = signal(false);
  readonly deactivatingId = signal<string | null>(null);
  readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    this.loadBroadcasts();
  }

  loadBroadcasts(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.broadcastService.getAdminBroadcasts().subscribe({
      next: (response) => {
        this.broadcasts.set(response.data ?? []);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(error.message || 'Failed to load broadcasts.');
        this.loading.set(false);
      },
    });
  }

  sendBroadcast(): void {
    if (this.sending()) {
      return;
    }

    this.broadcastForm.markAllAsTouched();
    const expiresAt = this.resolveExpiresAt();

    if (this.broadcastForm.invalid || !expiresAt) {
      return;
    }

    this.sending.set(true);
    this.broadcastService.createBroadcast({
      message: this.broadcastForm.controls.message.value,
      expiresAt: expiresAt.toISOString(),
    }).subscribe({
      next: (response) => {
        const created = response.data;
        if (created) {
          this.broadcasts.update(broadcasts => [created, ...broadcasts]);
        }
        this.broadcastForm.controls.message.reset('');
        this.sending.set(false);
        this.snackbar.success('Broadcast sent.');
      },
      error: () => {
        this.sending.set(false);
      },
    });
  }

  deactivateBroadcast(id: string): void {
    if (this.deactivatingId()) {
      return;
    }

    this.deactivatingId.set(id);
    this.broadcastService.deactivateBroadcast(id).subscribe({
      next: (response) => {
        const updated = response.data;
        if (updated) {
          this.broadcasts.update(broadcasts =>
            broadcasts.map(broadcast => broadcast.id === updated.id ? updated : broadcast)
          );
        }
        this.deactivatingId.set(null);
        this.snackbar.success('Broadcast deactivated.');
      },
      error: () => {
        this.deactivatingId.set(null);
      },
    });
  }

  isCustomExpiry(): boolean {
    return this.broadcastForm.controls.expiryPreset.value === 'custom';
  }

  isExpired(broadcast: BroadcastMessage): boolean {
    return new Date(broadcast.expiresAt).getTime() <= Date.now();
  }

  statusLabel(broadcast: BroadcastMessage): string {
    if (!broadcast.active) {
      return 'Inactive';
    }

    return this.isExpired(broadcast) ? 'Expired' : 'Active';
  }

  private resolveExpiresAt(): Date | null {
    const preset = this.broadcastForm.controls.expiryPreset.value;

    if (preset === 'custom') {
      const customValue = this.broadcastForm.controls.customExpiresAt.value;
      if (!customValue) {
        this.broadcastForm.controls.customExpiresAt.setErrors({ required: true });
        return null;
      }

      const customDate = new Date(customValue);
      if (Number.isNaN(customDate.getTime()) || customDate.getTime() <= Date.now()) {
        this.broadcastForm.controls.customExpiresAt.setErrors({ future: true });
        return null;
      }

      this.broadcastForm.controls.customExpiresAt.setErrors(null);
      return customDate;
    }

    this.broadcastForm.controls.customExpiresAt.setErrors(null);
    const option = this.expiryOptions.find(item => item.value === preset);
    if (!option?.minutes) {
      return null;
    }

    return new Date(Date.now() + option.minutes * 60_000);
  }
}
