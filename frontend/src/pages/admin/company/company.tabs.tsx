import { Tabs } from 'antd';
import type { TabsProps } from 'antd';
import Access from '@/components/share/access';
import { ALL_PERMISSIONS } from '@/config/permissions';
import CompanyPage from './company';
import CompanyDeleted from './soft.delete';

const CompanyTabs = () => {
    const onChange = (key: string) => {
        // console.log(key);
    };

    const items: TabsProps['items'] = [
        {
            key: '1',
            label: 'Manage Companies',
            children: <CompanyPage />,
        },
        {
            key: '2',
            label: 'Deleted',
            children: <CompanyDeleted />,
        },

    ];
    return (
        <div>
            <Access
                permission={ALL_PERMISSIONS.COMPANIES.GET_PAGINATE}
            >
                <Tabs
                    defaultActiveKey="1"
                    items={items}
                    onChange={onChange}
                />
            </Access>
        </div>
    );
}

export default CompanyTabs;