import { Component, inject, signal, computed } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { SnackbarService } from '../../../services/snackbar.service';

@Component({
  selector: 'app-signup',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    RouterLink
  ],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss',
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private snackbarService = inject(SnackbarService);
  private router = inject(Router);

  signupForm: FormGroup;
  otpForm: FormGroup;

  isLoading = signal(false);
  showPassword = signal(false);
  showOtpStep = signal(false);
  userEmail = signal('');

  constructor() {
    this.signupForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(5)]],
    });

    this.otpForm = this.fb.group({
      otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });
  }

  getEmailError() {
    // const emailControl = this.signupForm.get('email');
    // if (emailControl?.hasError('required')) return 'Email is required';
    // if (emailControl?.hasError('email')) return 'Please enter a valid email';
    return '';
  }

  getPasswordError() {
    // const passwordControl = this.signupForm.get('password');
    // if (passwordControl?.hasError('required')) return 'Password is required';
    // if (passwordControl?.hasError('minlength')) return 'Password must be at least 5 characters';
    return '';
  }

  getOtpError() {
    // const otpControl = this.otpForm.get('otp');
    // if (otpControl?.hasError('required')) return 'OTP is required';
    // if (otpControl?.hasError('pattern')) return 'OTP must be 6 digits';
    return '';
  }

  togglePassword() {
    this.showPassword.update(v => !v);
  }

  onSignup() {
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { email, password } = this.signupForm.value;

    this.authService.signup({ email, password }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.userEmail.set(email);
        this.showOtpStep.set(true);
        this.snackbarService.success(response.data || 'OTP sent to your email!');
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackbarService.error(error.message || 'Signup failed. Please try again.');
      }
    });
  }

  onVerifyOtp() {
    if (this.otpForm.invalid) {
      this.otpForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const { otp } = this.otpForm.value;

    this.authService.verifyOtp({ email: this.userEmail(), otp }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.snackbarService.success(response.data || 'Email verified successfully!');
        // Navigate to login or home after successful verification
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackbarService.error(error.message || 'OTP verification failed. Please try again.');
      }
    });
  }

  resendOtp() {
    this.isLoading.set(true);
    const { email, password } = this.signupForm.value;

    this.authService.signup({ email, password }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.snackbarService.success('New OTP sent to your email!');
      },
      error: (error) => {
        this.isLoading.set(false);
        this.snackbarService.error(error.message || 'Failed to resend OTP.');
      }
    });
  }

  backToSignup() {
    this.showOtpStep.set(false);
    this.otpForm.reset();
  }
}
