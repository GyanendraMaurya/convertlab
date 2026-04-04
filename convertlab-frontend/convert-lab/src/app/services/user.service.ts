import { inject, Injectable } from "@angular/core";
import { ApiResponse, HttpService } from "./http.service";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";

@Injectable({ providedIn: 'root' })
export class UserService {

    private readonly apiUrl = environment.apiUrl;
    private readonly httpService = inject(HttpService);

    deleteAccount(): Observable<ApiResponse<void>> {
        return this.httpService.delete<ApiResponse<void>>(`${this.apiUrl}/user`);
    }


}