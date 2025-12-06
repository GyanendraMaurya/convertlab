export class PdfUploadResponse {
  constructor(public fileId: string, public pageCount: number) {
  }
}

export class PdfExtractRequest {
  constructor(public fileId: string, public pagesToKeep: string) {
  }
}
