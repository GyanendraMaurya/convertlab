import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { SessionService } from '../services/session.service';

export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
    const session = inject(SessionService);
    const sessionId = session.sessionId();

    if (!sessionId) {
        return next(req);
    }

    const cloned = req.clone({
        setHeaders: {
            'X-Session-Id': sessionId
        }
    });

    return next(cloned);
};