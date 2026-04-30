import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse, HttpService } from './http.service';

export interface ContactInquiryRequest {
  fullName: string;
  email: string | null;
  phone: string | null;
  message: string;
  inquiryType: string | null;
  budgetRange: string | null;
}

export interface ContactInquiryResponse {
  id: string;
  createdAt: string;
  emailNotificationStatus: 'PENDING' | 'SENT' | 'FAILED';
}

@Injectable({
  providedIn: 'root',
})
export class ContactService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  createInquiry(request: ContactInquiryRequest): Observable<ApiResponse<ContactInquiryResponse>> {
    return this.httpService.post<ApiResponse<ContactInquiryResponse>>(
      `${this.apiUrl}/contact/inquiries`,
      request,
    );
  }
}
