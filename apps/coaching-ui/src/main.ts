import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AppComponent } from './app/app.component';
import { webSocketInterceptor } from './app/interceptors/websocket.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(
      withInterceptors([webSocketInterceptor])
    ),
    provideAnimations(),
  ],
}).catch(err => console.error(err));
