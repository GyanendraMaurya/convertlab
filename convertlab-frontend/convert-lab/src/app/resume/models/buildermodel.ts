export interface ResumeTemplate {
  id: string;
  name: string;
  description: string;
}

export interface ResumeExperience {
  role: string;
  company: string;
  location: string;
  startDate: string;
  endDate: string;
  current: boolean;
  points: string[];
}

export interface ResumeEducation {
  degree: string;
  institution: string;
  location: string;
  duration: string;
  details: string;
}

export interface ResumeProject {
  name: string;
  description: string;
  url: string;
  points: string[];
}

export interface ResumeLink {
  label: string;
  url: string;
}

export type ResumeCustomSectionPlacement = 'sidebar' | 'main';

export interface ResumeCustomSectionItem {
  title: string;
  subtitle: string;
  points: string[];
}

export interface ResumeCustomSection {
  title: string;
  placement: ResumeCustomSectionPlacement;
  items: ResumeCustomSectionItem[];
}

export interface ResumeRequest {
  fullName: string;
  title: string;
  email: string;
  phone: string;
  location: string;
  summary: string;
  photoDataUri: string;
  skills: string[];
  experience: ResumeExperience[];
  education: ResumeEducation[];
  projects: ResumeProject[];
  links: ResumeLink[];
  customSections: ResumeCustomSection[];
}
