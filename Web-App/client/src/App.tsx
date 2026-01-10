import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { SocketProvider } from './contexts/SocketContext';
import { DeviceProvider } from './contexts/DeviceContext';
import { AuthProvider } from './contexts/AuthContext';
import Dashboard from './pages/Dashboard';
import DeviceDetail from './pages/DeviceDetail';
import Login from './pages/Login';
import ProtectedRoute from './components/ProtectedRoute';

function App() {
    return (
        <AuthProvider>
            <SocketProvider>
                <DeviceProvider>
                    <BrowserRouter>
                        <Routes>
                            <Route path="/" element={<Dashboard />} />
                            <Route path="/login" element={<Login />} />
                            <Route path="/device/:id" element={
                                <ProtectedRoute>
                                    <DeviceDetail />
                                </ProtectedRoute>
                            } />
                        </Routes>
                    </BrowserRouter>
                </DeviceProvider>
            </SocketProvider>
        </AuthProvider>
    );
}

export default App;
