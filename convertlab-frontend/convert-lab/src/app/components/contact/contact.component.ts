import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { SeoService } from '../../seo/seo.service';

interface InquiryOption {
  value: string;
  label: string;
}

interface ContactDetail {
  icon: string;
  label: string;
  value: string;
  href?: string;
}

function contactMethodValidator(control: AbstractControl): ValidationErrors | null {
  const email = String(control.get('email')?.value ?? '').trim();
  const phone = String(control.get('phone')?.value ?? '').trim();

  return email || phone ? null : { contactMethodRequired: true };
}

function optionalPhoneValidator(control: AbstractControl): ValidationErrors | null {
  const value = String(control.value ?? '').trim();
  const digitCount = value.replace(/\D/g, '').length;
  const phoneLike = /^\+?[0-9\s().-]+$/.test(value);

  if (!value) {
    return null;
  }

  return phoneLike && digitCount >= 7 && digitCount <= 15 ? null : { phone: true };
}

@Component({
  selector: 'app-contact',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
  ],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss',
})
export class ContactComponent {
  private fb = inject(FormBuilder);
  private seoService = inject(SeoService);

  readonly isSubmitting = signal(false);
  readonly submitted = signal(false);
  readonly showSuccess = signal(false);

  readonly inquiryTypes: InquiryOption[] = [
    { value: 'website', label: 'Website or landing page' },
    { value: 'web-app', label: 'Web app or dashboard' },
    { value: 'ai', label: 'AI or document workflow' },
    { value: 'maintenance', label: 'Fixes or improvements' },
    { value: 'other', label: 'Something else' },
  ];

  readonly budgetRanges: InquiryOption[] = [
    { value: 'not-sure', label: 'Not sure yet' },
    { value: 'small', label: 'Small project' },
    { value: 'medium', label: 'Medium build' },
    { value: 'ongoing', label: 'Ongoing work' },
  ];

  readonly contactDetails: ContactDetail[] = [
    {
      icon: 'mail',
      label: 'Email',
      value: 'gmaurya973@gmail.com',
      href: 'mailto:gmaurya973@gmail.com',
    },
    {
      icon: 'business_center',
      label: 'LinkedIn',
      value: 'linkedin.com/in/gyanendramaurya',
      href: 'https://www.linkedin.com/in/gyanendramaurya/',
    },
    {
      icon: 'schedule',
      label: 'Response time',
      value: 'Usually within 24-48 hours',
    },
  ];

  readonly contactForm = this.fb.group(
    {
      fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
      email: ['', [Validators.email, Validators.maxLength(120)]],
      phone: ['', [optionalPhoneValidator]],
      inquiryType: ['web-app'],
      budgetRange: ['not-sure'],
      message: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1500)]],
    },
    { validators: [contactMethodValidator] },
  );

  contactMethodError(): boolean {
    const hasError = this.contactForm.hasError('contactMethodRequired');
    const emailTouched = Boolean(this.contactForm.get('email')?.touched);
    const phoneTouched = Boolean(this.contactForm.get('phone')?.touched);
    return hasError && (emailTouched || phoneTouched || this.submitted());
  }

  ngOnInit() {
    this.seoService.applySEO('contact');
  }

  ngOnDestroy() {
    this.seoService.cleanup();
  }

  getFieldError(fieldName: string): string {
    const control = this.contactForm.get(fieldName);

    if (!control || !control.touched || !control.errors) {
      return '';
    }

    if (control.hasError('required')) {
      return 'This field is required.';
    }

    if (control.hasError('email')) {
      return 'Enter a valid email address.';
    }

    if (control.hasError('phone')) {
      return 'Enter a valid contact number.';
    }

    if (control.hasError('minlength')) {
      const requiredLength = control.errors['minlength'].requiredLength;
      return `Use at least ${requiredLength} characters.`;
    }

    if (control.hasError('maxlength')) {
      const requiredLength = control.errors['maxlength'].requiredLength;
      return `Keep this under ${requiredLength} characters.`;
    }

    return '';
  }

  onSubmit() {
    this.submitted.set(true);
    this.showSuccess.set(false);

    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);

    setTimeout(() => {
      this.isSubmitting.set(false);
      console.info('Contact inquiry ready for backend integration:', this.contactForm.getRawValue());
      this.showSuccess.set(true);
      this.contactForm.reset({
        fullName: '',
        email: '',
        phone: '',
        inquiryType: 'web-app',
        budgetRange: 'not-sure',
        message: '',
      });
      this.submitted.set(false);
    }, 600);
  }
}
