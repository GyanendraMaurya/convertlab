import { HttpContextToken } from '@angular/common/http';

export const SUPPRESS_ERROR = new HttpContextToken<boolean>(() => false);
export const IS_BLOB_REQUEST = new HttpContextToken<boolean>(() => false);
