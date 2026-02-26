import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from '@/components/Layout';
import Dashboard from '@/pages/Dashboard';
import Contacts from '@/pages/Contacts';
import ContactDetail from '@/pages/ContactDetail';
import Compose from '@/pages/Compose';
import Templates from '@/pages/Templates';
import Partnerships from '@/pages/Partnerships';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/contacts" element={<Contacts />} />
          <Route path="/contacts/:id" element={<ContactDetail />} />
          <Route path="/compose" element={<Compose />} />
          <Route path="/templates" element={<Templates />} />
          <Route path="/partnerships" element={<Partnerships />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
