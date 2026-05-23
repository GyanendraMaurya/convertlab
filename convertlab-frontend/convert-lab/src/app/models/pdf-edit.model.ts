export interface PdfEditRequest {
  fileId: string;
  operations: PdfEditOperation[];
}

export interface PdfEditOperation {
  pageNumber: number;
  x: number;
  y: number;
  width: number;
  height: number;
  text: string;
  fontFamily: PdfEditFontFamily;
  fontSize: number;
  bold: boolean;
  italic: boolean;
  textColor: string;
  coverColor: string;
  coverEnabled: boolean;
  alignment: PdfEditAlignment;
}

export type PdfEditAlignment = 'left' | 'center' | 'right';
export type PdfEditFontFamily = 'Helvetica' | 'Times' | 'Courier';
