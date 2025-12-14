export class PdfUploadResponse {
  constructor(public fileId: string, public pageCount: number, fileName: string, thumbnailUrl: string) {
  }
}

export class PdfExtractRequest {
  constructor(public fileId: string, public pageRange: string, public actionType: ActionType) {
  }
}

export enum ActionType {
  KEEP = 'KEEP',
  REMOVE = 'REMOVE'
}
