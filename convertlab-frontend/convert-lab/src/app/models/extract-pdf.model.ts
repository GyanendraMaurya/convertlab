

export class PdfExtractRequest {
  constructor(public fileId: string, public pageRange: string, public actionType: ActionType) {
  }
}

export enum ActionType {
  KEEP = 'KEEP',
  REMOVE = 'REMOVE'
}
