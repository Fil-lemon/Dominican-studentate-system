import React, { useEffect, useState } from 'react';
import useHttp from '../../services/UseHttp';
import { Role } from '../../models/Interfaces';
import { backendUrl } from '../../utils/constants';
import { useParams, useNavigate } from 'react-router-dom';
import LoadingSpinner from "../../components/LoadingScreen";
import '../../components/AddEditForm.css';
import RoleFormFields from "./RoleFormFields";
import ConfirmDeletionPopup from "../../components/ConfirmDeletionPopup";

function EditRole() {
    const { roleId } = useParams();
    const navigate = useNavigate();
    const { request: fetchRole, error: fetchError} = useHttp(`${backendUrl}/api/roles/${roleId}`, 'GET');
    const { request: updateRole, error: updateError , loading: updateLoading} = useHttp(`${backendUrl}/api/roles/${roleId}`, 'PUT');
    const { request: deleteRole, error: deleteError, loading: deleteLoading } = useHttp(`${backendUrl}/api/roles/${roleId}`, 'DELETE');
    const [roleData, setRoleData] = useState<Role | null>(null);
    const [validationError, setValidationError] = useState<string>('');
    const [showConfirmationPopup, setShowConfirmationPopup] = useState<boolean>(false);

    useEffect(() => {
        if (roleId) {
            fetchRole(null, (data: Role) => {
                setRoleData(data);
            });
        }
    }, [roleId, fetchRole]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!roleData?.name || !roleData?.type) {
            setValidationError("Wszystkie pola muszą być wypełnione.");
            return;
        }

        updateRole(roleData, () => {
            navigate('/roles', { state: { message: 'Pomyślnie zaktualizowano rolę' } });
        });
    };

    const handleDelete = () => {
        if (roleId) {
            deleteRole(null, () => {
                navigate('/roles', { state: { message: 'Pomyślnie usunięto rolę' } });
            })
                .then(() => setShowConfirmationPopup(false));
        }
    };

    if (!roleData) return <LoadingSpinner/>;

    return (
        <div className="fade-in">
            <div className="page-header">
                <h1>Edytuj rolę</h1>
            </div>
            <div className="edit-entity-container">
                {(fetchError || updateError || deleteError) && <div className="alert alert-danger">{fetchError || updateError || deleteError}</div>}
                {validationError && <div className="alert alert-danger">{validationError}</div>}
                <form onSubmit={handleSubmit} className="needs-validation" noValidate>
                    <RoleFormFields roleData={roleData} setRoleData={setRoleData} />
                    <div className="d-flex justify-content-between">
                        <button className="btn btn-success" type="submit" disabled={updateLoading || deleteLoading}>Zaktualizuj</button>
                        <button type="button" onClick={() => setShowConfirmationPopup(true)} className="btn btn-danger" disabled={updateLoading || deleteLoading}>Usuń</button>
                        {showConfirmationPopup && <ConfirmDeletionPopup onHandle={handleDelete} onClose={() => setShowConfirmationPopup(false)}/>}
                    </div>
                </form>
            </div>
        </div>
    );
}

export default EditRole;
