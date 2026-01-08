import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { SocketProvider } from './contexts/SocketContext';
import { DeviceProvider } from './contexts/DeviceContext';
import Dashboard from './pages/Dashboard';
import DeviceDetail from './pages/DeviceDetail';

function App() {
    return (
        <SocketProvider>
            <DeviceProvider>
                <BrowserRouter>
                    <Routes>
                        <Route path="/" element={<Dashboard />} />
                        <Route path="/device/:id" element={<DeviceDetail />} />
                    </Routes>
                </BrowserRouter>
            </DeviceProvider>
        </SocketProvider>
    );
}

export default App;
