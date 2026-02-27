export interface PdfPasswordRequest {
    fileId: string;
    password: string;
    action: Action;
}

export enum Action {
  ADD = 'ADD',
  REMOVE = 'REMOVE'
}
