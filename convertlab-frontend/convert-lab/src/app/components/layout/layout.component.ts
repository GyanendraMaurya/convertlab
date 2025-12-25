import { Component } from '@angular/core';
import { NavbarComponent } from "../shared/navbar/navbar.component";
import { RouterOutlet } from '@angular/router';
import { VersionDisplayComponent } from '../shared/version-display';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, NavbarComponent, VersionDisplayComponent],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {

}
