import { Component, signal, output, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-page-range-input',
  imports: [
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './page-range-input.component.html',
  styleUrl: './page-range-input.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PageRangeInputComponent),
      multi: true
    }
  ],
})
export class PageRangeInputComponent implements ControlValueAccessor {


  inputValue = signal('');
  errorMessage = signal('');
  onChange: (value: string) => void = () => { };
  onTouched = () => { };

  writeValue(obj: string): void {
    this.inputValue.set(obj);
    this.onChange(obj);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }
  registerOnTouched(fn: any): void {
    // throw new Error('Method not implemented.');
  }
  setDisabledState?(isDisabled: boolean): void {

    // throw new Error('Method not implemented.');
  }

}
