export interface SplitPdfRequest {
  fileId: string;
  pageRange: string;
  splitType: SplitType;
}

export enum SplitType {
  EACH_PAGE = 'EACH_PAGE',
  BY_RANGE = 'BY_RANGE'
}
