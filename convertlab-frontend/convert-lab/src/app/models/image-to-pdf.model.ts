export interface ImageToPdfRequest {
  images: Image[]
}

export interface Image {
  fileId: string | null
  rotation: 0 | 90 | 180 | 270
}
